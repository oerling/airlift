package io.airlift.http.client.jetty;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.io.CountingInputStream;
import io.airlift.http.client.GatheringByteArrayInputStream;
import io.airlift.http.client.HeaderName;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpFields;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;

class JettyResponse
        implements io.airlift.http.client.Response
{
    private final Response response;
    private final CountingInputStream inputStream;
    private final long totalBytes;
    private final List<byte[]> buffers;
    private final ListMultimap<HeaderName, String> headers;
    public JettyResponse(Response response, InputStream inputStream)
    {
        this.response = response;
        this.inputStream = new CountingInputStream(inputStream);
        this.headers = toHeadersMap(response.getHeaders());
        if (inputStream instanceof GatheringByteArrayInputStream) {
            GatheringByteArrayInputStream stream = (GatheringByteArrayInputStream) inputStream;
            totalBytes = stream.getTotalBytes();
            buffers = new ArrayList();
            for (byte[] buffer : stream.get()) {
                buffers.add(buffer);
            }
        }
        else {
            buffers = null;
            totalBytes = 0;
        }
    }

    @Override
    public int getStatusCode()
    {
        return response.getStatus();
    }

    @Override
    public String getStatusMessage()
    {
        return response.getReason();
    }

    @Override
    public ListMultimap<HeaderName, String> getHeaders()
    {
        return headers;
    }

    @Override
    public long getBytesRead()
    {
        return inputStream.getCount();
    }

    @Override
    public InputStream getInputStream()
    {
        return inputStream;
    }

    @Override
    public boolean supportsGetBuffers()
    {
        return buffers != null;
    }

    @Override
    public List<byte[]> getBuffers()
    {
        return buffers;
    }
    @Override
    public long getTotalBytes()
    {
        return totalBytes;
    }
    
    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("statusCode", getStatusCode())
                .add("statusMessage", getStatusMessage())
                .add("headers", getHeaders())
                .toString();
    }

    private static ListMultimap<HeaderName, String> toHeadersMap(HttpFields headers)
    {
        ImmutableListMultimap.Builder<HeaderName, String> builder = ImmutableListMultimap.builder();
        for (String name : headers.getFieldNamesCollection()) {
            for (String value : headers.getValuesList(name)) {
                builder.put(HeaderName.of(name), value);
            }
        }
        return builder.build();
    }
}
