package com.melardev.spring.services;

import com.melardev.spring.entities.Todo;
import com.melardev.spring.repositories.TodosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static com.melardev.spring.config.DbConfig.DB_SCHEDULER;

@Service
public class TodoService {
    @Autowired
    private TodosRepository todosRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;


    public Flux<Todo> findAllHqlSummary(int page, int pageSize) {
        Pageable pageRequest = PageRequest.of(page, pageSize);
        Flux<Todo> defer = Flux.defer(() -> Flux.fromIterable(this.todosRepository.findAllHqlSummary(pageRequest)));
        return defer.subscribeOn(DB_SCHEDULER);
    }

    public Flux<Todo> findAllPending(int page, int pageSize) {
        Pageable pageRequest = PageRequest.of(page, pageSize);
        Flux<Todo> defer = Flux.defer(() -> Flux.fromIterable(this.todosRepository.findByHqlPending(pageRequest)));
        return defer.subscribeOn(DB_SCHEDULER);
    }

    public Flux<Todo> findAllCompleted(int page, int pageSize) {
        Pageable pageRequest = PageRequest.of(page, pageSize);
        Flux<Todo> defer = Flux.defer(() -> Flux.fromIterable(this.todosRepository.findByHqlCompleted(pageRequest)));
        return defer.subscribeOn(DB_SCHEDULER);
    }

    public Mono<Optional<Todo>> findById(Long id) {
        Mono<Optional<Todo>> todo = Mono
                .defer(() -> Mono.just(this.todosRepository.findById(id)))
                .subscribeOn(DB_SCHEDULER);

        return todo;
    }

    public Mono<Long> getCompletedCount() {
        return Mono
                .defer(() -> Mono.just(this.todosRepository.getCompletedCount()))
                .subscribeOn(DB_SCHEDULER);
    }

    public Mono<Long> getPendingCount() {
        return Mono
                .defer(() -> Mono.just(this.todosRepository.getPendingCount()))
                .subscribeOn(DB_SCHEDULER);
    }

    public Mono<Todo> save(Todo todo) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Todo persistedTodo = todosRepository.save(todo);
            return persistedTodo;
        })).subscribeOn(DB_SCHEDULER);
    }

    public Mono<Void> deleteAll() {
        return Mono.fromCallable((Callable<Void>) () -> {
            todosRepository.deleteAll();
            return null;
        }).subscribeOn(DB_SCHEDULER);


    }

    public Mono<Boolean> delete(Optional<Todo> todo) {
        if (!todo.isPresent())
            return Mono.empty();

        return Mono.defer((Supplier<Mono<Boolean>>) () -> {
            todosRepository.delete(todo.get());
            return Mono.just(true);
        }).subscribeOn(DB_SCHEDULER);
    }

    public Mono<Long> count() {
        return Mono.defer(() -> Mono.just(todosRepository.count())).subscribeOn(DB_SCHEDULER);
    }

    public Flux<Todo> saveAll(Set<Todo> todos) {
        return Flux.defer(() -> Flux.fromIterable(this.todosRepository.saveAll(todos))).subscribeOn(DB_SCHEDULER);
    }
}
