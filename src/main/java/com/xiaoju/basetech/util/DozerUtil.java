package com.xiaoju.basetech.util;

import com.github.dozermapper.core.Mapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 对象转换工具类
 * @author wukun
 * @date: 2022/11/23 5:55 PM
 * @sprint: 0
 */
@Component
public class DozerUtil {
    //dozer-spring-boot-stater中注入的对象
    @Resource
    private Mapper mapper;

    public DozerUtil(Mapper mapper){
        this.mapper=mapper;
    }

    //单个对象转换
    public <T> T map(Object source, Class<T> destination){
        return mapper.map(source,destination);
    }

    //List 对象转换
    public <T> List<T> mapList(List<?> sourceList, Class<T> destination){
        if(sourceList==null||destination==null){
            return null;
        }else{
            //遍历转换
            List<T> list=new ArrayList<>();
            for(Object o:sourceList){
                T obj=map(o,destination);
                list.add(obj);
            }
            return list;
        }
    }

    public <T> List<T> mapList(Object obj, Class<T> destination){
        List<T> result = new ArrayList<>();
        if(obj instanceof List<?>){
            for (Object o : (List<?>) obj){
                result.add(map(o, destination));
            }
            return result;
        }
        return new ArrayList<>();
    }


}
