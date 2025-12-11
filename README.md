Repository: https://github.com/Fariz-98/library-system

# Tech Stack
- **Language and framework**
   - Java 17
   - Spring Boot 4

- **Build**
   - Maven

- **Database**
   - MySQL 8

- **Testing**
   - JUnit 5
   - Mockito
   - Spring `@WebMvcTest` for controller tests
   - AssertJ for assertions

- **Other**
   - OpenAPI / Swagger annotations
   - Docker and Docker Compose
   - GitHub Actions for running tests on every push

# Docker
Can be run using docker compose:
```
docker compose up --build
```
## Dockerfile
Consists of two stages:
1. Builder stage
   1. Copies `pom.xml`
   2. Copies `src` and runs `mvn clean package -DskipTests`
2. Runtime stage
   1. Copies built jar from builder
   2. Runs `java -jar app.jar`
## Docker Compose
The compose file uses the `.env` file to load environment variables.
In this repo, `.env` is committed with simple non-sensitive default values so the app is easy to run and test in a local/dev environment.
In a real setup, secrets ***should not*** be committed to Git. Instead, it is loaded from a proper secrets management solution

Extras:
- Uses `mysql:8.0` image
- Uses `healthcheck` to prevent Spring from booting up before MySQL

# CI with GitHub Actions
There is a test CI workflow on every push
- Triggers on any push to any branch
- Uploads surefire test reports

# Entity / Data Models
**Borrower**

- `id`
- `name`
- `email` (unique)

**Book**

- `id`
- `isbn`
- `title`
- `author`
- `status` (`AVAILABLE` or `BORROWED`)

Each row in `books` is one physical copy. If the library has three copies of "Clean Code", there will be three rows with the same ISBN, title and author, but different IDs

**Loan**

- `id`
- `book` (FK)
- `borrower` (FK)
- `status` (`ACTIVE` or `RETURNED`)
- `borrowedAt`
- `returnedAt` (nullable)

`Loan` is the history table.

# Assumptions / Extra Requirements
- Every request into the app is assumed to be authenticated behind an auth layer.
  - In Spring, this can be behind a filter that checks for auth cookie and build a `Principal` object with proper Authorisation
- Concurrency in borrowing a book
  - Two borrowers might call the POST request to borrow a book at roughly the same time
  - The solution used in this project is pessimistic write lock on the `Book` row when borrowing:
   ```
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT b FROM Book b WHERE b.id = :id")
   Optional<Book> findByIdForUpdate(@Param("id") Long id);
   ```
  - Now only one transaction can pass the availability check and update at a time
- Loan Data Model
  - This data model is used to save the history of book borrowing and returning
- Logging
  - `INFO` is used at the start of main operations
  - `WARN` is used when business rules are violated
  - `ERROR` reserved for real errors like unexpected exceptions or system issues
- Testing
  - Unit tests are written for services
  - Controller tests are written for controllers with `@WebMvcTest`

# Future Improvements
- Caching 
  - Right now every request hits the DB. A cache like Redis could help later for:
    - Frequently accessed catalogue data
    - Aggregated views such as number of available copies per ISBN
- Search and filter for books by title, author, ISBN and status
- Search and filter for borrowers by email and name
- Extend CI pipeline into full CD:
  - Build and push Docker image to a registry (e.g. ECR)
  - Deploy to a container platform (e.g. ECS, EKS)

# API Endpoints / How to use
On top of this documentation:
- Swagger UI is also available once the application is running at `/swagger-ui/index.html`
- A Postman collection is also provided in this repo `Library-System.postman_collection.json` for manually testing the API endpoints locally
  - Postman collection local base URL is set to: `localhost:8080`

## Error Structure
- All errors use this common structure:
```
{
  "timestamp": "2025-12-10T12:34:56.789",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Validation failed",
  "path": "/api/books",
  "details": [
    "isbn: ISBN is required",
    "title: Title is required"
  ]
}
```
- `timestamp` time when the error object was built
- `status` HTTP code
- `error` HTTP status name
- `message` short message for the client
- `path` request URI
- `details` extra messages, used for validation errors

## Borrower
### Register a new borrower
POST `/api/borrowers`

Request body:
```
{
  "name": "string",
  "email": "user@example.com"
}
```

Validation:
- `name` is required and must not be blank
- `email` is required, must not be blank and must be a valid email

Business rules:
- `email` must be unique. If it already exists the service throws `DuplicateActionException("Email is taken")`

Responses:
- `201 CREATED`
  - Body:
```
{
  "id": 0,
  "name": "string",
  "email": "string"
}
```
- `400 BAD_REQUEST`
  - Validation errors
- `409 CONFLICT`
  - Message: `Email is taken`

## Book
### Register a new book copy
POST `/api/books`

Request Body:
```
{
  "isbn": "string",
  "title": "string",
  "author": "string"
}
```

Validation:
- All fields are required and must not be blank

Business rules:
- You can add more copies with the same ISBN, but:
  - If a book with the same ISBN already exists, the title and author must match
  - If they do not match, we throw `BusinessException("ISBN already exists with different title/author")`

Responses:
- `201 CREATED`
  - Body:
```
{
  "id": 0,
  "isbn": "string",
  "title": "string",
  "author": "string",
  "bookStatus": "AVAILABLE"
}
```
- `400 BAD_REQUEST`
  - Validation error with message `Validation failed`
  - ISBN conflict with message `ISBN already exists with different title/author`

### Get all books
GET `/api/books`

Query params:
- `page` default `0`
- `size` default `20`

Example Response:
```
{
  "content": [
    {
      "id": 1,
      "isbn": "123123123",
      "title": "Title",
      "author": "Author",
      "bookStatus": "BORROWED"
    },
    {
      "id": 2,
      "isbn": "978-1",
      "title": "Clean Code",
      "author": "Robert C. Martin",
      "bookStatus": "AVAILABLE"
    }
  ],
  "empty": false,
  "first": true,
  "last": true,
  "number": 0,
  "numberOfElements": 2,
  "pageable": {
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 20,
    "paged": true,
    "sort": {
      "empty": true,
      "sorted": false,
      "unsorted": true
    },
    "unpaged": false
  },
  "size": 20,
  "sort": {
    "empty": true,
    "sorted": false,
    "unsorted": true
  },
  "totalElements": 2,
  "totalPages": 1
}
```

## Loan
### Borrow a book
POST `/api/borrowers/{borrowerId}/borrow/{bookId}`

Responses:
- `200 OK`
  - Body:
```
{
  "id": 0,
  "bookId": 0,
  "bookIsbn": "string",
  "bookTitle": "string",
  "bookAuthor": "string",
  "borrowerId": 0,
  "borrowerName": "string",
  "borrowerEmail": "string",
  "status": "ACTIVE",
  "borrowedAt": "2025-12-11T05:26:35.569Z",
  "returnedAt": null
}
```
- `404 NOT_FOUND`
  - Borrower not found
  - Book not found
- `409 CONFLICT`
  - `Book is already borrowed`

### Return a book
POST `/api/borrowers/{borrowerId}/return/{bookId}`

Responses:
- `200 OK`
  - Body:
```
{
  "id": 0,
  "bookId": 0,
  "bookIsbn": "string",
  "bookTitle": "string",
  "bookAuthor": "string",
  "borrowerId": 0,
  "borrowerName": "string",
  "borrowerEmail": "string",
  "status": "RETURNED",
  "borrowedAt": "2025-12-11T05:28:38.777Z",
  "returnedAt": "2025-12-11T05:28:38.777Z"
}
```
- `404 NOT_FOUND`
  - Borrower not found
  - Book not found
- `400 BAD_REQUEST`
  - `Book is not currently borrowed`
  - `This book is currently borrowed by a different person`