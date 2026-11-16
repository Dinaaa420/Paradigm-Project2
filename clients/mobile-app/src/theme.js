import { useColorScheme } from 'react-native';

const light = {
  bg: '#f7f5f2', card: '#ffffff', text: '#1d1b19', muted: '#6f6a64', border: '#e4dfd8',
  red: '#b3261e', green: '#0f6b3d', onAccent: '#ffffff', errorBg: '#fdecea',
};
const dark = {
  bg: '#1b1a19', card: '#252322', text: '#f1eeea', muted: '#a39d96', border: '#3a3734',
  red: '#e0574e', green: '#2f9e66', onAccent: '#ffffff', errorBg: '#3a2220',
};

export function useTheme() {
  return useColorScheme() === 'dark' ? dark : light;
}
