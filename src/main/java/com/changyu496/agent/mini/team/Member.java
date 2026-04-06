package com.changyu496.agent.mini.team;

import lombok.Data;

@Data
public class Member {
    private String name;
    private String role;
    /**
     * idle/working/shutdown
     */
    private String status;
}
