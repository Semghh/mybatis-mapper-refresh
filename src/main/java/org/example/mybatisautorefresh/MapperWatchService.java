package org.example.mybatisautorefresh;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisXMLMapperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author SEMGHH
 * @date 2025/7/28 17:43
 */
@Slf4j
public class MapperWatchService {

    AtomicReference<WatchService> watchServiceAtomicReference = new AtomicReference<>();

    ConcurrentHashMap<String, Resource> nameResource = new ConcurrentHashMap<>();

    Set<Path> registered = new HashSet<>();

    MybatisConfiguration configuration;

    public MapperWatchService(MybatisConfiguration configuration) {
        this.configuration = configuration;
    }

    public void WatchFile(String url, Resource resource) throws IOException {
        this.WatchFile(Path.of(url), resource);
    }


    public void WatchFile(Path path, Resource resource) throws IOException {
        WatchService watchService = watchServiceAtomicReference.get();
        if (watchService == null) {
            createWatchService();
        }

        Path registerPath = null;
        String pathStr = path.toString();
        int index;
        if ((index = pathStr.indexOf("target\\classes")) != -1) {
            Path p = Path.of(pathStr.substring(index + 15));
            Path srcPath = new File("").getAbsoluteFile().toPath().resolve("src").resolve("main").resolve("resources").resolve(p);

            if (srcPath.toFile().exists()) {
                registerPath = srcPath;
            }
        }
        if (registerPath == null) {
            log.info("资源：[{}]无法找到对应的源码路径。", path);
            return;
        }
        resource = new FileSystemResource(registerPath.resolve(resource.getFilename()).toString());

        if (!registered.contains(registerPath)) {
            registerPath.register(watchServiceAtomicReference.get(), StandardWatchEventKinds.ENTRY_MODIFY);
            registered.add(registerPath);
            log.info("监听路径: [{}]", registerPath);
            nameResource.putIfAbsent(resource.getFilename(), resource);
        }
    }


    private void createWatchService() throws IOException {

        WatchService watchService = FileSystems.getDefault().newWatchService();
        final WatchService ws;
        boolean b = watchServiceAtomicReference.compareAndSet(null, watchService);
        ws = b ? watchService : watchServiceAtomicReference.get();

        new Thread(() -> {
            while (true) {
                try {

                    WatchKey poll = ws.poll(200, TimeUnit.MILLISECONDS);
                    if (poll == null) {
                        continue;
                    }
                    List<WatchEvent<?>> watchEvents = poll.pollEvents();
                    for (WatchEvent<?> watchEvent : watchEvents) {
                        Object context = watchEvent.context();
                        if (context instanceof Path p) {
                            try {
                                refreshMapper(p);
                            } catch (Throwable e) {
                                if (e.getCause() instanceof SAXParseException) {
                                    //ignore
                                    continue;
                                }
                                log.error("未知异常", e);
                            }
                        }
                    }
                    poll.reset();
                } catch (Throwable e) {
                    log.error("", e);
                }
            }
        }, "MapperRefreshThread").start();

    }


    private void refreshMapper(Path p) throws IOException, NoSuchFieldException, IllegalAccessException, ClassNotFoundException {


        String string = p.getFileName().getFileName().toString();
        if (string.endsWith("~")) {
            string = string.substring(0, string.length() - 1);
        }
        Resource resource = nameResource.get(string);

        if (resource==null){
            log.info("{}文件发生变动，未注册Mapper,忽略", p);
            return;
        }
        log.info("{}文件发生变动，刷新Mapper", resource.getURL());

        //获得XML的XNode
        XPathParser context = new XPathParser(resource.getInputStream(), true, configuration.getVariables(), new XMLMapperEntityResolver());


        String namespace = context.evalNode("/mapper").getStringAttribute("namespace");


        //移除MappedStatement
        ConcurrentHashMap<String, MappedStatement> mappedStatements = Permit.getObjFromField(MybatisConfiguration.class, "mappedStatements", configuration);

        Iterator<String> iterator = mappedStatements.keys().asIterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            if (next.startsWith(namespace)) {
                mappedStatements.remove(next);
            }
        }


        //移除sqlFragments
        ConcurrentHashMap<String, XNode> sqlFragments = Permit.getObjFromField(MybatisConfiguration.class, "sqlFragments", configuration);
        for (XNode sqlFragment : context.evalNodes("/mapper/sql")) {
            sqlFragments.remove(namespace + "." + sqlFragment.getStringAttribute("id"));
        }


        //移除loadedResources
        Set<String> loadedResources = Permit.getObjFromField(MybatisConfiguration.class, "loadedResources", configuration);
        loadedResources.remove(resource.toString());


        //移除 cache
        ConcurrentHashMap<String, Cache> caches = Permit.getObjFromField(MybatisConfiguration.class, "caches", configuration);
        caches.remove(namespace);

        //移除parameterMap
        ConcurrentHashMap<String, ParameterMap> parameterMaps = Permit.getObjFromField(MybatisConfiguration.class, "parameterMaps", configuration);
        List<XNode> xNodes = context.evalNodes("/mapper/parameterMap");
        for (XNode parameterMapNode : xNodes) {
            String id = parameterMapNode.getStringAttribute("id");
            parameterMaps.remove(id);
        }


        MybatisXMLMapperBuilder xmlMapperBuilder = new MybatisXMLMapperBuilder(resource.getInputStream(),
                configuration, resource.toString(), configuration.getSqlFragments());
        xmlMapperBuilder.parse();


        log.info("{}刷新完成", resource.getURL());
    }


}
