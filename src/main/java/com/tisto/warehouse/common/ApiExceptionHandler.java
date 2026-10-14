package com.tisto.warehouse.common;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.tisto.warehouse.catalog.DuplicateSkuException;
import com.tisto.warehouse.inventory.InsufficientStockException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
				.toList();
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
		problem.setTitle("Validation failed");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail notFound(NotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
	}

	@ExceptionHandler(BadRequestException.class)
	ProblemDetail badRequest(BadRequestException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail conflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
	}

	@ExceptionHandler(InsufficientStockException.class)
	ProblemDetail insufficientStock(InsufficientStockException ex) {
		ProblemDetail problem = problem(HttpStatus.CONFLICT, "Insufficient stock", ex.getMessage());
		problem.setProperty("sku", ex.getSku());
		problem.setProperty("requested", ex.getRequested());
		problem.setProperty("available", ex.getAvailable());
		return problem;
	}

	@ExceptionHandler(DuplicateSkuException.class)
	ProblemDetail duplicateSku(DuplicateSkuException ex) {
		ProblemDetail problem = problem(HttpStatus.CONFLICT, "Duplicate SKU", ex.getMessage());
		problem.setProperty("sku", ex.getSku());
		return problem;
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ProblemDetail optimisticLock() {
		return problem(HttpStatus.CONFLICT, "Concurrent update",
				"The record was changed by someone else. Reload it and try again.");
	}

	// Last line of defence when two requests race past the SKU pre-check and hit the unique index.
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail integrityViolation() {
		return problem(HttpStatus.CONFLICT, "Data conflict", "The change conflicts with existing data");
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}
