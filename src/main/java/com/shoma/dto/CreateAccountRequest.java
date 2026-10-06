package com.shoma.dto;


import lombok.Data;

@Data
public class CreateAccountRequest {
    String ownerName;
    String password;
}
