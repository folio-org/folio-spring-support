package org.folio.spring.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;

public class MultiReadHttpServletRequestWrapper extends HttpServletRequestWrapper {

  private ByteArrayOutputStream cachedBytes;

  public MultiReadHttpServletRequestWrapper(HttpServletRequest request) {
    super(request);
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    if (cachedBytes == null)
      cacheInputStream();

    return new CachedServletInputStream(cachedBytes.toByteArray());
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return new BufferedReader(new InputStreamReader(getInputStream()));
  }

  private void cacheInputStream() throws IOException {
    cachedBytes = new ByteArrayOutputStream();
    IOUtils.copy(super.getInputStream(), cachedBytes);
  }

  private static class CachedServletInputStream extends ServletInputStream {

    private final ByteArrayInputStream buffer;

    public CachedServletInputStream(byte[] contents) {
      this.buffer = new ByteArrayInputStream(contents);
    }

    @Override
    public int read() {
      return buffer.read();
    }

    @Override
    public boolean isFinished() {
      return buffer.available() == 0;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener listener) {
      throw new NotImplementedException("Not implemented");
    }
  }
}

