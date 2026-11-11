package com.jlpp.modules.exam.dto;

public record SectionProgress(String section, String label, long linked, int required) {

    public boolean complete() { return linked == required; }

    public boolean over() { return linked > required; }
}
