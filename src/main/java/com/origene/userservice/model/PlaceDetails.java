package com.origene.userservice.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "placeDetails")
public class PlaceDetails {
  private String country;
  private String countryCode;
  private String areaLevelOne;
  private String areaLevelTwo;
  private String areaLevelThree;
  private String locality;
  private String subLocality;
  private String plusCode;
  private String postalCode;
  private Object geometry;
}