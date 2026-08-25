# Backend Dev Technical Test

This application provides an endpoint to retrieve the details of products similar to a given product.

## Running with Docker

You can run the application using Docker with the following commands:

- `docker build -t my-app .`
- `docker run -d --rm --name my-app --network backenddevtest-main_default -e EXTERNAL_API_BASE_URL=http://backenddevtest-main-simulado-1:80 -p 5000:5000 my-app`

## Running with Maven

Alternatively, you can run the application directly with Maven:

- `.\mvnw.cmd spring-boot:run`

## Observations

1. When fetching the details of similar products, an error for any individual product will fail the entire request.

   This behavior is intentional because the provided k6 tests expect product ID `4` to return a `404` and product ID `5`
   to return a `500`.

2. Requests for product ID `3` intentionally take longer than the default Spring MVC request timeout and will result in
   a timeout exception.

   However, the similar products returned by this request can be verified by increasing both the Spring MVC request
   timeout and the WebClient timeout.