package com.example.kafkaclient;

import org.bson.types.ObjectId;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HelloReactiveRepo extends ReactiveCrudRepository<Hello, ObjectId> {
}
