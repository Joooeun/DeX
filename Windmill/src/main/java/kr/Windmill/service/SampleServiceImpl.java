package kr.Windmill.service;


import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.Windmill.mapper.SampleMapper;

@Service
@Transactional
public class SampleServiceImpl implements SampleService {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(SampleServiceImpl.class);
    
    @Autowired
    SampleMapper sampleMapper;
    
    public List<Map<String, String>> findSampleList(Map<String, String> map) {
    	return sampleMapper.findSampleList(map);
    }
    
}
