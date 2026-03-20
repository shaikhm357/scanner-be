import { WebSocket } from 'ws';
import { execSync } from 'child_process';
import { readFileSync, existsSync } from 'fs';

// Generate a small blank JPEG using ImageMagick or Python PIL
const testImagePath = '/tmp/test-frame.jpg';
if (!existsSync(testImagePath)) {
  try {
    execSync(`convert -size 100x100 xc:white ${testImagePath}`);
  } catch {
    // Fallback: use Python PIL
    execSync(`python3 -c "
from PIL import Image
img = Image.new('RGB', (100,100), 'white')
img.save('${testImagePath}', 'JPEG')
"`);
  }
}

const ws = new WebSocket('ws://localhost:8080/api/v1/decode/stream');

ws.on('open', () => {
  console.log('Connected');
});

ws.on('message', (data) => {
  const msg = JSON.parse(data.toString());
  console.log('Received:', JSON.stringify(msg, null, 2));

  // After session_started, send a binary frame
  if (msg.type === 'session_started' && !ws._sentFrame) {
    ws._sentFrame = true;
    const imageBytes = readFileSync(testImagePath);
    console.log(`Sending ${imageBytes.length} byte JPEG frame...`);
    ws.send(imageBytes);
  }

  // After receiving result, send config then close
  if (msg.type === 'result') {
    console.log(`Decode took ${msg.processingTimeMs}ms, found ${msg.barcodesFound} barcodes`);
    ws.close();
  }
});

ws.on('close', () => {
  console.log('Disconnected');
  process.exit(0);
});

ws.on('error', (err) => {
  console.error('Error:', err.message);
  process.exit(1);
});
