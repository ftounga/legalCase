package fr.ailegalcase.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

class JsonResponseWriter {

    private JsonResponseWriter() {}

    static void write(HttpServletResponse response, ObjectMapper objectMapper,
                      int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
