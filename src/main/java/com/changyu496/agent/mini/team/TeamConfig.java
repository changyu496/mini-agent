package com.changyu496.agent.mini.team;

import lombok.Data;

import java.util.List;

@Data
public class TeamConfig {

    private String teamName = "default";

    private List<Member> members;

}
