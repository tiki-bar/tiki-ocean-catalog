/*
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

package com.mytiki.ocean.catalog.drop;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.mytiki.ocean.catalog.utils.ApiExceptionBuilder;
import com.mytiki.ocean.catalog.utils.Iceberg;
import com.mytiki.ocean.catalog.utils.Mapper;
import com.mytiki.ocean.catalog.utils.Router;
import org.apache.iceberg.catalog.TableIdentifier;
import software.amazon.awssdk.http.HttpStatusCode;

public class DropHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent request, Context context) {
        String name = Router.extract(
                request.getRequestContext().getHttp().getPath(),
                "(?<=/api/latest/)(\\S*[^/])");
        Iceberg iceberg = new Iceberg();
        TableIdentifier identifier = TableIdentifier.of(Iceberg.database, name);
        if(!iceberg.tableExists(identifier)){
            throw new ApiExceptionBuilder(HttpStatusCode.BAD_REQUEST)
                    .message("Bad Request")
                    .detail("Table does not exist")
                    .properties("name", name)
                    .build();
        }
        if(!iceberg.dropTable(identifier)){
            throw new ApiExceptionBuilder(HttpStatusCode.INTERNAL_SERVER_ERROR)
                    .message("Drop Failed")
                    .properties("name", name)
                    .build();
        }
        iceberg.close();
        DropRsp body = new DropRsp();
        body.setName(name);
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(HttpStatusCode.OK)
                .withBody(new Mapper().writeValueAsString(body))
                .build();
    }
}
