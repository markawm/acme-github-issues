Run with: `mvn compile quarkus:dev`

Use ngrok to expose the local service (running at http://localhost:8080) to the GitHub webhooks.

Or... you can just post sample files directly to perform local testing. For example:
`curl -v -X POST -H "Content-Type: application/json" -d @src/test/resources/sample-issue.json http://localhost:8080
/webhook`