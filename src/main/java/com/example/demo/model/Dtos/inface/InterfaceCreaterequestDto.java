package com.example.demo.model.Dtos.inface;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterfaceCreaterequestDto {
    private String name;
    private String description;
    private String startLocation;
    private String endLocation;
}
