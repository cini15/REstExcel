package com.example.demo.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
@Getter
@Setter
@ToString
public class Disinfection {
    private boolean isNew;
    private String inspection;
    private String number;
    private String date1;
    private String date2;
    private String name1;
    private String name2;
    private String quantity;
    private String conclusion1;
    private String conclusion2;
    private String conclusion3;
    private String method_disinfection;
    private String organization;
    private String fio1;
    private String fio2;
    private String fio3;
}
