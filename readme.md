# Backend Dev Technical Test

This application provides an endpoint to retrieve the details of products similar to a given product.

## Running with Docker

You can run the application using Docker with the following commands:

- `docker build -t my-app .`
- `docker run -d --rm --name my-app --network backenddevtest-main_default -e EXTERNAL_API_BASE_URL=http://backenddevtest-main-simulado-1:80 -p 5000:5000 my-app`

## Running with Maven

Alternatively, you can run the application directly with Maven:

- `.\mvnw.cmd spring-boot:run`