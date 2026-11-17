-- BYAHERO by HDDEV — Supabase PostgreSQL + Realtime Schema
-- Run this in your Supabase SQL Editor to enable multi-device authentication,
-- ride synchronization, and rider/passenger role switching.

create extension if not exists "uuid-ossp";

create type byahero_role as enum ('passenger', 'rider', 'both');
create type byahero_mode as enum ('passenger', 'rider');
create type byahero_vehicle as enum ('motorcycle', 'taxi');
create type byahero_ride_status as enum (
  'searching',
  'accepted',
  'arriving',
  'in_progress',
  'completed',
  'cancelled'
);

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null,
  full_name text not null,
  avatar_url text,
  role byahero_role not null default 'passenger',
  active_mode byahero_mode not null default 'passenger',
  vehicle_type byahero_vehicle not null default 'motorcycle',
  plate_number text default '',
  is_online boolean not null default false,
  current_lat double precision,
  current_lng double precision,
  rating numeric(3,2) not null default 5.00,
  total_rides integer not null default 0,
  rider_earnings_php numeric(10,2) not null default 0,
  rider_incentives_php numeric(10,2) not null default 0,
  updated_at timestamptz not null default now()
);

create table if not exists public.rides (
  id uuid primary key default uuid_generate_v4(),
  passenger_id uuid references public.profiles(id) on delete set null,
  rider_id uuid references public.profiles(id) on delete set null,
  passenger_name text not null,
  rider_name text,
  rider_plate text,
  vehicle_type byahero_vehicle not null,
  pickup_address text not null,
  pickup_lat double precision not null,
  pickup_lng double precision not null,
  destination_address text not null,
  destination_lat double precision not null,
  destination_lng double precision not null,
  distance_km numeric(8,2) not null,
  duration_mins integer not null,
  base_fare numeric(10,2) not null,
  discount_amount numeric(10,2) not null default 0,
  final_fare numeric(10,2) not null,
  rider_incentive numeric(10,2) not null default 0,
  is_saturday_promo boolean not null default false,
  status byahero_ride_status not null default 'searching',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.rides enable row level security;

create policy "Users can view profiles"
  on public.profiles for select
  using (true);

create policy "Users can insert own profile"
  on public.profiles for insert
  with check (auth.uid() = id);

create policy "Users can update own profile"
  on public.profiles for update
  using (auth.uid() = id);

create policy "Authenticated users can view active or own rides"
  on public.rides for select
  using (
    status = 'searching'
    or auth.uid() = passenger_id
    or auth.uid() = rider_id
  );

create policy "Passengers can create rides"
  on public.rides for insert
  with check (auth.uid() = passenger_id);

create policy "Passengers and assigned riders can update rides"
  on public.rides for update
  using (
    auth.uid() = passenger_id
    or auth.uid() = rider_id
    or status = 'searching'
  );

-- Enable Supabase Realtime across Android & Web devices
alter publication supabase_realtime add table public.profiles;
alter publication supabase_realtime add table public.rides;
