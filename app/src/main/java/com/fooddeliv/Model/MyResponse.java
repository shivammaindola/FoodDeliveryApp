package com.fooddeliv.Model;

import java.util.List;

public class MyResponse {
    public long multicast_id;
    public int success;
    public int failure;
    public int canonical_ida;
    public List<Result> results;
}
