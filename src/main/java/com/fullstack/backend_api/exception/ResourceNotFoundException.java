package com.fullstack.backend_api.exception;

// HTTP 404를 나타내기 위해 ResponseStatus를 사용할 수 있지만,
// 여기서는 Service Layer에서 발생하고 ControllerAdvice가 처리한다고 가정합니다.

public class ResourceNotFoundException extends RuntimeException {

    // 어떤 종류의 리소스를 어떤 값으로 찾으려 했는지 메시지에 포함하는 것이 좋습니다.
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s, 찾을 수 없습니다. %s : '%s'", resourceName, fieldName, fieldValue));
    }
}
