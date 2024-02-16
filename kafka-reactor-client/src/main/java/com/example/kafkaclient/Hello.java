package com.example.kafkaclient;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
@Document
public class Hello {

  @MongoId
  private ObjectId id;

  private String text;
}
