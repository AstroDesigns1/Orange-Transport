package com.astrotech.transport.mappers;

import com.astrotech.transport.core.TrimWhiteSpace;
import com.astrotech.transport.dto.request.TerminalRequest;
import com.astrotech.transport.dto.response.*;
import com.astrotech.transport.entities.*;

import java.util.List;

public class TerminalMapper {
    public static Terminal createTerminal(TerminalRequest request, User user, Region region){
        var state = TrimWhiteSpace.trimWhiteSpace(request.state());
        var city = TrimWhiteSpace.trimWhiteSpace(request.city());
        var name = TrimWhiteSpace.trimWhiteSpace(request.terminalName());
        return Terminal.builder()
                .state(state)
                .name(name)
                .city(city)
                .region(region)
                .address(request.address())
                .terminalSupervisor(user)
                .build();

    }
    public static TerminalResponse toResponse(Terminal terminal){
        var userResponse = UserMapper.response(terminal.getTerminalSupervisor());
        return new TerminalResponse(
                simpleResponse(terminal),
                userResponse
        );
    }
    public static SimpleTerminalResponse simpleResponse(Terminal terminal){

        return new SimpleTerminalResponse(
                terminal.getId(),
                terminal.getName(),
                terminal.getState(),
                terminal.getCity(),
                terminal.getAddress()
        );
    }
    public static List<SimpleTerminalResponse> simpleResponseList(List<Terminal> terminals) {
        if (terminals == null || terminals.isEmpty()) {
            return List.of();
        }
        return terminals.stream()
                .map(TerminalMapper::simpleResponse)
                .toList();
    }

}
