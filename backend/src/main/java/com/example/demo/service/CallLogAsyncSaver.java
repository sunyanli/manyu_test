package com.example.demo.service;

import com.example.demo.model.CallLog;
import com.example.demo.repository.CallLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class CallLogAsyncSaver {

    @Autowired
    private CallLogRepository callLogRepository;

    @Async
    public void save(CallLog log) {
        callLogRepository.save(log);
    }
}