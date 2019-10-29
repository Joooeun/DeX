package kr.dbrain.Windmill.mapper;

import java.util.List;
import java.util.Map;

public interface SampleMapper {

    public List<Map<String, String>> findSampleList(Map<String, String> map);
    
}
