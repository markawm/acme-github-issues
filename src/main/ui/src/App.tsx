import React from 'react';
import '@cloudbees/honeyui/dist/index.css';
import { Badge } from '@cloudbees/honeyui-react';

const App: React.FC = () => {
  return (
    <div className="App">
      <header className="App-header">
        <Badge>HoneyUI</Badge>
      </header>
    </div>
  );
};

export default App;
