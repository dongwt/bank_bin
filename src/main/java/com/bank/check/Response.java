package com.bank.check;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by dongwt on 2018/2/5.
 */
@Data
public class Response implements Serializable{

    private static final long serialVersionUID = -4122683723270620046L;

    private String bank;

    private boolean validated;

    private String cardType;

    private String key;

    private List<Message> messages;
}
