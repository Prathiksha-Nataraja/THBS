package com.torryharris.employee.crud.verticles;

import com.torryharris.employee.crud.controller.EmployeeController;
import com.torryharris.employee.crud.model.Employee;
import com.torryharris.employee.crud.model.Response;
import com.torryharris.employee.crud.util.ConfigKeys;
import com.torryharris.employee.crud.util.PropertyFileUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ApiServer extends AbstractVerticle {
  private static final Logger logger = LogManager.getLogger(ApiServer.class);
  private static Router router;
  private EmployeeController employeeController;



  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    employeeController = new EmployeeController(vertx);
    router = Router.router(vertx);

    List<Employee> employees = new ArrayList<>();



    // Attach a BodyHandler to parse request body and set upload to false
    router.route().handler(BodyHandler.create(false));

    router.get("/employees")
      .handler(routingContext -> employeeController.getEmployees().future()
        .onSuccess(response -> sendResponse(routingContext, response))
      );

//    String id=new String();

    router.get("/employees/:id")
      .handler(routingContext ->{
        String id= routingContext.request().getParam("id");
        employeeController.getEmployeebyId(id).future()
            .onSuccess(response -> sendResponse(routingContext,response));
        }
      );

    router.post("/employees")
      .handler(routingContext ->{
        Employee employee = Json.decodeValue(routingContext.getBody(), Employee.class);
        HttpServerResponse serverResponse=routingContext.response();
        employeeController.postEmployees(employee).future()
          .onSuccess(response -> sendResponse(routingContext, response));
        employees.add(employee);
        System.out.println(routingContext.getBodyAsJson());
        routingContext.response().end(Json.encodePrettily(employees));


        }
      );

    router.put("/employees/:id")
      .handler(routingContext ->{
        Employee employee = Json.decodeValue(routingContext.getBody(), Employee.class);
          employeeController.updateEmpId(employee).future()
            .onSuccess(response -> sendResponse(routingContext,response));
        employees.add(employee);
        routingContext.response().end(Json.encodePrettily(employees));
        }
      );


    router.delete("/employees/:id")
      .handler(routingContext ->{
          String id= routingContext.request().getParam("id");
          HttpServerResponse serverResponse = routingContext.response();
          employeeController.deleteEmployeebyId(id).future()
            .onSuccess(response -> sendResponse(routingContext,response));
          serverResponse.end(" Employee deleted successfully...");

        }
      );

    router.post("/login")
      .handler(routingContext->{
        String authuser= routingContext.request().getHeader(HttpHeaders.AUTHORIZATION);
        authuser= authuser.substring(6);
        String Str= new String(Base64.getDecoder().decode(authuser));
         String[] val = Str.split(":");
          System.out.println(val[0]);
        System.out.println(val[1]);
        String username=val[0];
        String password= val[1];
        HttpServerResponse serverResponse = routingContext.response();
        employeeController.login(username,password).future()
          .onSuccess(response->sendResponse(routingContext,response));
//        System.out.println(routingContext.getBodyAsJson());
//        JsonObject json = new JsonObject()
//          .put("message","Employee login success");
//        routingContext.response()
//          .putHeader("content-type","application/json;charset=UTF8")
//          .end(json.encodePrettily());
        serverResponse.end(" Successful login...");

      });

    HttpServerOptions options = new HttpServerOptions().setTcpKeepAlive(true);
    vertx.createHttpServer(options)
      .exceptionHandler(logger::catching)
      .requestHandler(router)
      .listen(Integer.parseInt(PropertyFileUtils.getProperty(ConfigKeys.HTTP_SERVER_PORT)))
      .onSuccess(httpServer -> {
        logger.info("Server started on port {}", httpServer.actualPort());
        startPromise.tryComplete();
      })
      .onFailure(startPromise::tryFail);
  }

  private void sendResponse(RoutingContext routingContext, Response response) {
    response.getHeaders().stream()
      .forEach(entry -> routingContext.response().putHeader(entry.getKey(), entry.getValue().toString()));
    routingContext.response().setStatusCode(response.getStatusCode())
      .end(response.getResponseBody());
  }
}
