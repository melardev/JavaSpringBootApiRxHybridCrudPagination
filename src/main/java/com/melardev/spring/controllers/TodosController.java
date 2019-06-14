package com.melardev.spring.controllers;

import com.melardev.spring.dtos.responses.*;
import com.melardev.spring.entities.Todo;
import com.melardev.spring.services.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Optional;
import java.util.function.Function;

@CrossOrigin
@RestController
@RequestMapping("todos")
public class TodosController {

    @Autowired
    TodoService todoService;

    @GetMapping
    public Mono<? extends AppResponse> getAll(ServerHttpRequest request,
                                              @RequestParam(value = "page", defaultValue = "1") int page,
                                              @RequestParam(value = "page_size", defaultValue = "5") int pageSize) {
        Flux<Todo> todos = todoService.findAllHqlSummary(page, pageSize);
        Mono<AppResponse> response = getResponseFromTodosFlux(todos, todoService.count(), request, page, pageSize);
        return response;
    }


    @GetMapping("/pending")
    public Mono<? extends AppResponse> getPending(ServerHttpRequest request,
                                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                                  @RequestParam(value = "page_size", defaultValue = "5") int pageSize) {
        Flux<Todo> todos = todoService.findAllPending(page, pageSize);
        return getResponseFromTodosFlux(todos, todoService.getPendingCount(), request, page, pageSize);
    }


    @GetMapping("/completed")
    public Mono<? extends AppResponse> getCompleted(ServerHttpRequest request,
                                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                                    @RequestParam(value = "page_size", defaultValue = "5") int pageSize) {
        Flux<Todo> todos = todoService.findAllCompleted(page, pageSize);
        return getResponseFromTodosFlux(todos, todoService.getCompletedCount(), request, page, pageSize);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<? extends AppResponse>> get(@PathVariable("id") Long id) {
        return this.todoService.findById(id)
                .map(optionalTodo -> {
                    if (optionalTodo.isPresent()) {
                        Todo todo = optionalTodo.get();
                        return ResponseEntity.ok(new TodoDetailsResponse(todo));
                    } else
                        return new ResponseEntity<ErrorResponse>(new ErrorResponse("Todo not found"), HttpStatus.NOT_FOUND);
                });
    }


    @PostMapping
    public Mono<ResponseEntity<? extends AppResponse>> create(@Valid @RequestBody Todo todo) {
        return todoService.save(todo)
                .map(savedTodo -> new ResponseEntity<>(new TodoDetailsResponse(savedTodo), HttpStatus.CREATED));
    }


    @PutMapping("/{id}")
    public Mono<ResponseEntity<? extends AppResponse>> update(@PathVariable("id") Long id, @RequestBody Todo todoInput) {
        // Do you know how to make it better? Let me know on Twitter or Pull request please.
        return todoService.findById(id)
                .map(new Function<Optional<Todo>, Mono<ResponseEntity<? extends AppResponse>>>() {
                    @Override
                    public Mono<ResponseEntity<? extends AppResponse>> apply(Optional<Todo> t) {
                        if (!t.isPresent())
                            return Mono.just(new ResponseEntity(new ErrorResponse("Not found"), HttpStatus.NOT_FOUND));

                        Todo todo = t.get();
                        String title = todoInput.getTitle();
                        if (title != null)
                            todo.setTitle(title);

                        String description = todoInput.getDescription();
                        if (description != null)
                            todo.setDescription(description);

                        todo.setCompleted(todoInput.isCompleted());
                        Mono<ResponseEntity<? extends AppResponse>> response = todoService.save(todo)
                                .flatMap(new Function<Todo, Mono<ResponseEntity<? extends AppResponse>>>() {
                                    @Override
                                    public Mono<ResponseEntity<? extends AppResponse>> apply(Todo todo) {
                                        return Mono.just(ResponseEntity.ok(new TodoDetailsResponse(todo)));
                                    }
                                });

                        return response;
                    }
                }).flatMap(new Function<Mono<ResponseEntity<? extends AppResponse>>, Mono<? extends ResponseEntity<? extends AppResponse>>>() {
                    @Override
                    public Mono<? extends ResponseEntity<? extends AppResponse>> apply(Mono<ResponseEntity<? extends AppResponse>> responseEntityMono) {
                        return responseEntityMono;
                    }
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<? extends AppResponse>> delete(@PathVariable("id") Long id) {
        return todoService.findById(id)
                .flatMap(ot -> todoService.delete(ot))
                .map(new Function<Boolean, ResponseEntity<? extends AppResponse>>() {
                    @Override
                    public ResponseEntity<? extends AppResponse> apply(Boolean bool) {
                        return new ResponseEntity<>(new SuccessResponse("Deleted successfully"), HttpStatus.OK);
                    }
                }).defaultIfEmpty(new ResponseEntity<>(new ErrorResponse("Todo not found"), HttpStatus.NOT_FOUND));
    }


    @DeleteMapping
    public Mono<ResponseEntity<? extends AppResponse>> deleteAll() {
        return todoService.deleteAll().then(Mono.just(new ResponseEntity<>(new SuccessResponse("Deleted successfully"), HttpStatus.OK)));
    }

    private Mono<AppResponse> getResponseFromTodosFlux(Flux<Todo> todoFlux, Mono<Long> countMono, ServerHttpRequest request, int page, int pageSize) {
        return todoFlux.collectList().flatMap(todoList -> countMono
                .map(totalItemsCount -> PageMeta.build(todoList, request.getURI().getPath(), page, pageSize, totalItemsCount))
                .map(pageMeta -> TodoListResponse.build(todoList, pageMeta)));
    }

}