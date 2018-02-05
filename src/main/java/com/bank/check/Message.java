package com.bank.check;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by dongwt on 2018/2/5.
 */
@Data
public class Message implements Serializable{

    private static final long serialVersionUID = -4124690365981633698L;

    private String errorCodes;

    private String name;
}
