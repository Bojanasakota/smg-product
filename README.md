# Product Management Application

## Scope

This application provides basic operations to manage products, including creating, updating, retrieving, and deleting products.

### Features

- **Create Product**  
  - **Endpoint:** `POST /api/products`  
  - **Request Body:**  
    ```json
    {
      "name": "string",
      "price": 0.00
    }
    ```  
  - **Response:** Returns the created product. When a product is created, a Kafka event is generated and published to the `product-topic`.

- **Update Product**  
  - **Endpoint:** `PUT /api/products/{id}`  
  - **Request Body:**  
    ```json
    {
      "name": "string",
      "price": 0.00
    }
    ```  
  - **Response:** Returns the updated product.

- **Get Product by ID**  
  - **Endpoint:** `GET /api/products/{id}`  
  - **Response:** Returns the product if it exists; otherwise, returns a "Not Found" status.

- **Get All Products**  
  - **Endpoint:** `GET /api/products`  
  - **Response:** Returns a paginated list of products.

- **Delete Product by ID**  
  - **Endpoint:** `DELETE /api/products/{id}`  
  - **Response:** Returns a 204 status if the product is successfully deleted or a 404 status if the product does not exist.

---

## Architecture

The application follows a layered architecture with the following components:
- **Controller Layer**: Manages HTTP requests and responses.
- **Service Layer**: Contains the core business logic.
- **Repository Layer**: Manages database interactions.

---

## Running the Application

This application requires PostgreSQL, Kafka, and Zookeeper. 

- **PostgreSQL** is hosted on AWS RDS.
- **Kafka, Zookeeper,** and the **application** run in Docker.

### Starting the Application

To start Kafka, Zookeeper, and the application, use the following command:

```bash
docker-compose up --build
```
Application is running on the port 8081
Sample of the postman request
***Endpoint***: POST `http://localhost:8081/api/products`
```json
    {
      "name": "test_product_name",
      "price": 999.99
    }
```
 

### Database connection details

To connect to the PostgreSQL database using a database tool, use the following details:

- **Host**: smg.c5wug2s0wxb8.eu-north-1.rds.amazonaws.com
- **Port**: 5432
- **Username**: admin_user
- **Password**: *Test123





