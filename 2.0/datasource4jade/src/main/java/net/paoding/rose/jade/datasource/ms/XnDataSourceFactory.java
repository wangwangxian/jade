package net.paoding.rose.jade.datasource.ms;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import net.paoding.rose.jade.annotation.DAO;

import net.paoding.rose.jade.annotation.SQLType;
import net.paoding.rose.jade.annotation.UseMaster;
import net.paoding.rose.jade.context.spring.SpringDataSourceFactory;
import net.paoding.rose.jade.dataaccess.DataSourceFactory;
import net.paoding.rose.jade.dataaccess.DataSourceHolder;
import net.paoding.rose.jade.statement.StatementMetaData;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 替换 SpringDataSourceFactory，可以实现主从分离
 */
public class XnDataSourceFactory implements DataSourceFactory, ApplicationContextAware {

    private SpringDataSourceFactory inner = new SpringDataSourceFactory();

    private Map<String, DataSourceHolder> masterCached = new HashMap<String, DataSourceHolder>();

    private Map<String, DataSourceHolder> slaveCached = new HashMap<String, DataSourceHolder>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.inner.setApplicationContext(applicationContext);
    }

    @Override
    public DataSourceHolder getHolder(StatementMetaData metaData,
                                      Map<String, Object> runtimeProperties) {
        DataSourceHolder holder = inner.getHolder(metaData, runtimeProperties);
        if (holder == null) {
            String catalog = metaData.getDAOMetaData().getDAOClass().getAnnotation(DAO.class).catalog();
            if (catalog != null && catalog.length() > 0) {
                Map<String, DataSourceHolder> cached = slaveCached;
                if (useMaster(metaData)) {
                    cached = masterCached;
                }
                holder = cached.get(catalog);
                if (holder == null) {
                    DataSource dataSource = new XnDataSource(this, catalog, useMaster(metaData));
                    holder = new DataSourceHolder(dataSource);
                    cached.put(catalog, holder);
                }
            }
        }
        return holder;
    }

    private boolean useMaster(StatementMetaData metaData) {
        if (metaData.getSQLType() == SQLType.WRITE) {
            return true;
        }
        if (metaData.getMethod().isAnnotationPresent(UseMaster.class)) {
            return true;
        }
        return false;
    }
}
