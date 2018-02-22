
package main.server.pojo;

import javax.annotation.Generated;
import com.google.gson.annotations.SerializedName;

@Generated("net.hexar.json2pojo")
@SuppressWarnings("unused")
public class ProcessTranslateToSQLRes {

    @SerializedName("sql")
    private String mSql;

    public String getSql() {
        return mSql;
    }

    public static class Builder {

        private String mSql;

        public ProcessTranslateToSQLRes.Builder withSql(String sql) {
            mSql = sql;
            return this;
        }

        public ProcessTranslateToSQLRes build() {
            ProcessTranslateToSQLRes ProcessTranslateToSQLRes = new ProcessTranslateToSQLRes();
            ProcessTranslateToSQLRes.mSql = mSql;
            return ProcessTranslateToSQLRes;
        }

    }

}
