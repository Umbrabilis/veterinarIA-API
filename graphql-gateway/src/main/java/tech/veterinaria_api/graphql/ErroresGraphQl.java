package tech.veterinaria_api.graphql;

import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import tech.veterinaria_api.common.AccesoDenegadoException;
import tech.veterinaria_api.common.RecursoNoEncontradoException;
import tech.veterinaria_api.common.ServicioRemotoException;

/** Traduce los errores de los microservicios a errores GraphQL con su clasificación. */
@Component
public class ErroresGraphQl extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        ErrorType tipo;
        String mensaje;
        if (ex instanceof AccesoDenegadoException) {
            tipo = ErrorType.FORBIDDEN;
            mensaje = ex.getMessage();
        } else if (ex instanceof RecursoNoEncontradoException) {
            tipo = ErrorType.NOT_FOUND;
            mensaje = ex.getMessage();
        } else if (ex instanceof ServicioRemotoException) {
            tipo = ErrorType.INTERNAL_ERROR;
            mensaje = "Un servicio interno no está disponible";
        } else {
            return null; // lo maneja el resolver por defecto, sin exponer detalles
        }
        return GraphqlErrorBuilder.newError(env).errorType(tipo).message(mensaje).build();
    }
}
