import { Component, signal } from '@angular/core';

interface Chip { q: string; a: string; }
interface Topic { bot: string; chips: Chip[]; }

const TOPICS: Record<string, Topic> = {
  menu: {
    bot: 'Welcome to MicroVault Help. Choose a topic from the left menu, or pick a quick question below.',
    chips: [
      { q: 'Where is my dashboard?', a: 'Open the sidebar and click Dashboard. On mobile, tap the menu icon first.' },
      { q: 'How do I add a transaction?', a: 'Go to Transactions → Add transaction. Fill date (no future dates), name, category, type, and amount.' },
      { q: 'What is Affordability?', a: 'Affordability estimates whether a purchase fits your income, expenses, and savings. Open it from the sidebar.' },
    ],
  },
  navigate: {
    bot: 'Use the left sidebar to move between pages. Here is a short map of the main sections.',
    chips: [
      { q: 'Go to Savings', a: 'Sidebar → Savings. You can add deposits and withdrawals with search and filters.' },
      { q: 'Go to Budget', a: 'Sidebar → Budget. Set category limits; spent amounts come from expense transactions.' },
      { q: 'Go to Settings', a: 'Sidebar → Settings. Update profile, change password, or reopen financial setup.' },
      { q: 'Logout path', a: 'Sidebar → Logout, then confirm to end your session and return to login.' },
    ],
  },
  savings: {
    bot: 'Savings tracks vault entries. Totals update from your history table.',
    chips: [
      { q: 'How do I record a deposit?', a: 'Savings → Add entry → choose Deposit, enter amount and date (today or earlier), then Save.' },
      { q: 'Why is my balance low?', a: 'Balance is the sum of all savings amounts. Check for withdrawals or missing deposits in the history table.' },
      { q: 'Can I filter savings?', a: 'Yes — use the search box and category filter, then paginate through results.' },
    ],
  },
  budget: {
    bot: 'Budget limits are per category. Affordability uses income, expenses, and part of savings.',
    chips: [
      { q: 'Set a food budget', a: 'Budget → Add budget → Category Food → enter monthly limit → Save.' },
      { q: 'Check if I can buy something', a: 'Open Affordability, enter item name and cost, then Check affordability.' },
      { q: 'What does Near limit mean?', a: 'You have used 80% or more of that category’s monthly limit.' },
    ],
  },
  goals: {
    bot: 'Goals track targets; Reports summarize a date range from your transactions.',
    chips: [
      { q: 'Create a goal', a: 'Goals → Add goal. Fill title, category, target, saved amount, and deadline. Save to track progress.' },
      { q: 'Generate a report', a: 'Reports → choose From/To (no future dates) → Generate. History keeps past reports with pagination.' },
    ],
  },
  admin: {
    bot: 'Roles: Member, Admin, Super Admin. Extra sidebar sections appear after admin login.',
    chips: [
      { q: 'What can Admin do?', a: 'Admins see the admin dashboard, manage members, review feedback, and publish news.' },
      { q: 'What can Super Admin do?', a: 'Everything an admin can do, plus Manage Admins — add, edit, and delete admin accounts.' },
      { q: 'Super Admin email?', a: 'The seeded super admin uses the email configured in the auth service.' },
    ],
  },
  account: {
    bot: 'Keep your profile and password up to date from Settings.',
    chips: [
      { q: 'Change password', a: 'Settings → Change password → enter current and new password, then Update password.' },
      { q: 'Forgot password', a: 'From login, open Forgot password, send a verification code, then set a new password.' },
    ],
  },
};

@Component({
  selector: 'app-help',
  standalone: true,
  templateUrl: './help.component.html',
  styleUrl: './help.component.scss',
})
export class HelpComponent {
  readonly topics = [
    { key: 'menu', label: 'Main menu' },
    { key: 'navigate', label: 'How to navigate' },
    { key: 'savings', label: 'My savings' },
    { key: 'budget', label: 'Budget & affordability' },
    { key: 'goals', label: 'Goals & reports' },
    { key: 'admin', label: 'Admin / roles' },
    { key: 'account', label: 'Account & security' },
  ];
  readonly topic = signal('menu');
  readonly messages = signal<{ text: string; who: 'bot' | 'user' }[]>([]);
  readonly chips = signal<Chip[]>([]);

  constructor() { this.load('menu'); }

  load(key: string): void {
    const pack = TOPICS[key] || TOPICS['menu'];
    this.topic.set(key);
    this.messages.set([{ text: pack.bot, who: 'bot' }]);
    this.chips.set(pack.chips);
  }

  ask(chip: Chip): void {
    this.messages.update((list) => [...list, { text: chip.q, who: 'user' }]);
    setTimeout(() => this.messages.update((list) => [...list, { text: chip.a, who: 'bot' }]), 200);
  }
}
