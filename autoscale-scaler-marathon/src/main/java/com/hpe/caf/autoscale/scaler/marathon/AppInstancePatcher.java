package com.hpe.caf.autoscale.scaler.marathon;

import com.google.gson.JsonObject;
import com.hpe.caf.api.autoscale.ScalerException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.net.URI;

public class AppInstancePatcher {

    private URI marathonUri;

    public AppInstancePatcher(final URI marathonUri){
        this.marathonUri = marathonUri;
    }

    public void patchInstances(final String appId, int instances) throws ScalerException {
        patchInstances(appId, instances, false);
    }

    private void patchInstances(final String appId, final int instances, final boolean force) throws ScalerException {

        final JsonObject details = new JsonObject();
        details.addProperty("id", appId);
        details.addProperty("instances", instances);

        try(final CloseableHttpClient client = HttpClientBuilder.create().build()){
            final HttpPatch patch = new HttpPatch(marathonUri);
            patch.setEntity(new StringEntity(details.toString(), ContentType.APPLICATION_JSON));
            final HttpResponse response = client.execute(patch);
            if(!force && response.getStatusLine().getStatusCode()==409){
                patchInstances(appId, instances, true);
            }
            if(response.getStatusLine().getStatusCode()!=200){
                throw new ScalerException(response.getStatusLine().getReasonPhrase());
            }
        }
        catch (Exception ex){
            throw new ScalerException(String.format("Exception patching %s to %s instances.", appId, instances), ex);
        }
    }
}
