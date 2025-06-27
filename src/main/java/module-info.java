
module zi.zircky.gtnhlauncher {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.desktop;
  requires scribejava.core;
  requires scribejava.apis;

  requires java.net.http;
  requires com.fasterxml.jackson.databind;
  requires jdk.httpserver;
  requires com.google.gson;
  requires java.logging;
  requires java.management;
  requires jdk.management;

  opens zi.zircky.gtnhlauncher.auth to com.google.gson;

  opens zi.zircky.gtnhlauncher to javafx.fxml;
  exports zi.zircky.gtnhlauncher;
  exports zi.zircky.gtnhlauncher.controller;
  opens zi.zircky.gtnhlauncher.controller to javafx.fxml;
  exports zi.zircky.gtnhlauncher.settings;
  opens zi.zircky.gtnhlauncher.settings to com.google.gson, javafx.fxml;
}
