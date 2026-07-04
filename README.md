# Uptime Logpush

[English](README.md) | [简体中文](README_zh.md)

## Introduction
Uptime Logpush is a mobile monitoring tool designed to integrate with Uptime Kuma. It fetches log files generated on your web server via webhooks and provides real-time status updates and system notifications directly on your local device. This application enables you to monitor the health of your server nodes without needing to constantly keep the web interface open.

## Features
- **Real-Time Log Streaming**: Actively listens to and fetches log data from your configured data source.
- **Status Parsing and Display**: Automatically parses log entries to display server status (UP/DOWN), response time, hostname, IP address, and monitored port.
- **Flexible Data Source Configuration**: Supports fetching data from a standard raw text log file URL or a direct Uptime Kuma Webhook endpoint.
- **Background Monitoring**: Utilizes a foreground service to keep the application active and synchronizing data even when the app runs in the background.
- **Customizable Polling Interval**: Allows users to manually set the update frequency (e.g., every 300 seconds) for the background API requests. High frequencies offer better real-time performance but slightly increase battery consumption.
- **System Notifications**: Triggers push notifications on your device notification bar whenever a new log line is detected.
- **Privacy Protection Mode**: Supports hiding the application from your device's recent tasks list for enhanced privacy and security.

## Navigation
The application features three main tabs accessible via the bottom navigation bar:
- **Logs**: The primary monitoring interface. Displays a real-time feed of server status updates, including UP/DOWN status, response times, and specific connection details (hostnames, ports).
- **History**: Displays historical log records for reviewing past server status changes.
- **Config**: Allows you to access and modify all system settings.

## Configuration Details
To set up the application, navigate to the **Config** tab, where the following options are available:

- **Data Source URL**: Enter your log fetch URL or Webhook address. This field supports both standard raw text formats and Uptime Kuma webhook payloads.
- **Background Monitoring**: Enable this toggle to allow the app to run a foreground service. This ensures real-time synchronization and notifications continue to work when the app is not in the active foreground.
- **Polling Interval**: Configure the time gap (in seconds) between each data fetching request. You can either choose from a preset list or input a custom value.
- **New Log Notifications**: Toggle this on to receive system-level notification alerts on your device whenever the monitored server produces a new log update.
- **Hide from Recent Apps**: Toggle this on to prevent the application from appearing in your system's recent application list for added privacy.

## Setup Guide
1. Download and install Uptime Logpush on your mobile device.
2. Open the app and navigate to the **Config** tab.
3. Input your Uptime Kuma webhook URL or log file address into the **Data Source URL** field.
4. Set your preferred **Polling Interval** (e.g., 300 seconds).
5. Ensure **Background Monitoring** is enabled to keep the service alive.
6. You should now see server statuses appearing in the **Logs** tab.
