package com.hamhama.dto;

import java.util.List;

public class SubstituteDTO {
    public String original;
    public List<Substitute> substitutes;

    public static class Substitute {
        public String name;
        public String reason;
    }
}

