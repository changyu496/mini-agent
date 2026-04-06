package com.changyu496.agent.mini.agent;

public class Function {

    private String name;

    private String arguments;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return "Function{" +
                "name='" + name + '\'' +
                ", arguments='" + arguments + '\'' +
                '}';
    }
}
