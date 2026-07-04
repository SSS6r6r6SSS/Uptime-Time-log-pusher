<?php
/**
 * Uptime Kuma Webhook Receiver - Pure Backend Version
 * Logs each notification as a single-line JSON entry.
 */

// Define log file path
$logFile = 'kuma_notifications.log';

// Only process POST requests
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Get raw input from Uptime Kuma
    $rawData = file_get_contents('php://input');
    $data = json_decode($rawData, true);

    if ($data) {
        // Inject server-side timestamp
        $data['received_at'] = date('Y-m-d H:i:s');
        
        // Convert to single-line JSON (no pretty print, no escaped slashes/unicode)
        $logEntry = json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        
        // Append to file with a newline
        file_put_contents($logFile, $logEntry . PHP_EOL, FILE_APPEND | LOCK_EX);
        
        // Respond to Kuma
        header('Content-Type: application/json');
        echo json_encode(['status' => 'ok']);
    } else {
        http_response_code(400);
        echo "Invalid JSON Payload";
    }
} else {
    // Return 405 for GET or other methods
    http_response_code(405);
    echo "Method Not Allowed. This endpoint expects POST requests from Uptime Kuma.";
}
