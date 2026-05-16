import { BrowserRouter, Routes, Route } from 'react-router-dom';
import AppLayout from './components/Layout/AppLayout';
import Home from './pages/Home';
import Tutor from './pages/Tutor';
import Chat from './pages/Chat';
import Explore from './pages/Explore';
import Digest from './pages/Digest';
import Milestone from './pages/Milestone';
import Care from './pages/Care';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/tutor" element={<Tutor />} />
          <Route path="/chat" element={<Chat />} />
          <Route path="/explore" element={<Explore />} />
          <Route path="/digest" element={<Digest />} />
          <Route path="/milestone" element={<Milestone />} />
          <Route path="/care" element={<Care />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
